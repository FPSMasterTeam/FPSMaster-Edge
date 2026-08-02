// fpsmaster-smtc - Windows System Media Transport Controls bridge DLL
// C++/WinRT native library for FPSMaster Edge music integration.
//
// Build: see CMakeLists.txt in this directory (cmake -A x64|Win32 && cmake --build).
//        CI builds both architectures in .github/workflows/ci-release.yml; the
//        resulting DLLs are injected into src/main/resources/native/windows/ at
//        package time and are deliberately NOT committed to the repository.
//
// The DLL exports a tiny C ABI that the Java side (JNA) calls.
// Callbacks from SMTC button events are dispatched to a function pointer
// provided by the Java side.
//
// Desktop (Win32 JVM) integration goes through the
// ISystemMediaTransportControlsInterop::GetForWindow(hwnd) path. The UWP
// GetForCurrentView() API requires a CoreWindow and therefore always fails
// in the Minecraft JVM; it is deliberately not used here.
//
// Threading: every export is called from a single dedicated Java thread
// ("FPSMaster-SMTC"), never from the game thread. That thread owns the WinRT
// apartment this DLL initializes, so the game thread is never joined to an
// apartment it did not ask for.

#include <windows.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <wchar.h>
#include <chrono>

// WinRT headers
#include <winrt/Windows.Foundation.h>
#include <winrt/Windows.Media.h>
#include <winrt/Windows.Storage.Streams.h>
#include <SystemMediaTransportControlsInterop.h>

using namespace winrt;
using namespace Windows::Media;
using namespace Windows::Storage::Streams;

// ---------------------------------------------------------------------------
// Callback types (Java sets these via JNA)
// ---------------------------------------------------------------------------
typedef void(__cdecl* ControlCallback)(int action);
static ControlCallback g_callback = nullptr;

// ---------------------------------------------------------------------------
// SMTC session state
// ---------------------------------------------------------------------------
static SystemMediaTransportControls g_smtc = nullptr;
static winrt::event_token g_buttonToken{};
static bool g_initialized = false;
static bool g_apartmentOwned = false;

// Last failure reason, surfaced to Java through smtc_get_last_error so a bug
// report can tell "no window found" apart from "GetForWindow returned E_FAIL".
static wchar_t g_lastError[256] = {0};

static void set_last_error(const wchar_t* msg) {
    if (!msg) {
        g_lastError[0] = 0;
        return;
    }
    wcsncpy_s(g_lastError, msg, _TRUNCATE);
}

static void set_last_error_hr(const wchar_t* msg, HRESULT hr) {
    swprintf_s(g_lastError, L"%s (hr=0x%08X)", msg, (unsigned int)hr);
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

namespace {

struct WindowSearch {
    DWORD pid;
    HWND result;
};

// Only accept a real top-level application window: owned by this process,
// visible, no owner window, and with a caption. Without these filters the very
// first EnumWindows hit during Forge start-up can be the splash screen or a
// stray AWT frame, and SMTC would bind its session to a window that is about to
// be destroyed.
BOOL CALLBACK enum_window_proc(HWND hwnd, LPARAM lParam) {
    auto* search = reinterpret_cast<WindowSearch*>(lParam);
    DWORD wpid = 0;
    GetWindowThreadProcessId(hwnd, &wpid);
    if (wpid != search->pid) return TRUE;
    if (!IsWindowVisible(hwnd)) return TRUE;
    if (GetWindow(hwnd, GW_OWNER) != nullptr) return TRUE;
    if (GetWindowTextLengthW(hwnd) == 0) return TRUE;

    search->result = hwnd;
    return FALSE;
}

} // namespace

// Find a top-level window owned by this process. GetForWindow needs a real
// HWND; the Java side passes 0 only when it could not obtain the LWJGL handle.
static HWND find_process_window() {
    WindowSearch search{GetCurrentProcessId(), nullptr};
    EnumWindows(enum_window_proc, reinterpret_cast<LPARAM>(&search));
    return search.result;
}

// ---------------------------------------------------------------------------
// C ABI exports
// ---------------------------------------------------------------------------

extern "C" {

/// Initialize the SMTC session for a desktop window. Call once; idempotent.
/// hwnd: parent window handle (0 = auto-detect the process main window).
/// Returns 1 on success, 0 on failure (so the Java side can surface it).
__declspec(dllexport) int __cdecl smtc_start(HWND hwnd) {
    if (g_initialized) return 1;
    set_last_error(nullptr);
    try {
        // Initialize the WinRT apartment on the calling thread (the dedicated
        // Java SMTC thread). MTA so button callbacks can fire on any thread and
        // we never need a message pump.
        try {
            winrt::init_apartment(winrt::apartment_type::multi_threaded);
            g_apartmentOwned = true;
        } catch (winrt::hresult_error const& e) {
            // RPC_E_CHANGED_MODE means the thread was already initialized as an
            // STA by someone else. SMTC works from an STA too, so carry on —
            // but do not claim ownership, since we must not uninitialize it.
            if (static_cast<HRESULT>(e.code()) != RPC_E_CHANGED_MODE) {
                set_last_error_hr(L"init_apartment failed", static_cast<HRESULT>(e.code()));
                g_initialized = false;
                return 0;
            }
            g_apartmentOwned = false;
        }

        HWND target = hwnd;
        if (target == nullptr) {
            target = find_process_window();
        }
        if (target == nullptr) {
            set_last_error(L"no top-level window found for this process");
            g_initialized = false;
            return 0;
        }

        // Desktop interop: get the transport controls bound to a window.
        auto interop = get_activation_factory<SystemMediaTransportControls,
                                              ISystemMediaTransportControlsInterop>();
        SystemMediaTransportControls controls = nullptr;
        HRESULT hr = interop->GetForWindow(target,
            winrt::guid_of<SystemMediaTransportControls>(),
            winrt::put_abi(controls));
        if (FAILED(hr)) {
            set_last_error_hr(L"GetForWindow failed", hr);
            g_initialized = false;
            return 0;
        }
        g_smtc = controls;
        if (!g_smtc) {
            set_last_error(L"GetForWindow returned a null interface");
            g_initialized = false;
            return 0;
        }

        // Enable all supported buttons
        g_smtc.IsPlayEnabled(true);
        g_smtc.IsPauseEnabled(true);
        g_smtc.IsNextEnabled(true);
        g_smtc.IsPreviousEnabled(true);

        // Subscribe to button events
        g_buttonToken = g_smtc.ButtonPressed(
            [](const SystemMediaTransportControls& sender,
               const SystemMediaTransportControlsButtonPressedEventArgs& args) {
                if (!g_callback) return;
                switch (args.Button()) {
                case SystemMediaTransportControlsButton::Play:
                case SystemMediaTransportControlsButton::Pause:
                    g_callback(1);  // play/pause toggle
                    break;
                case SystemMediaTransportControlsButton::Next:
                    g_callback(2);
                    break;
                case SystemMediaTransportControlsButton::Previous:
                    g_callback(3);
                    break;
                case SystemMediaTransportControlsButton::Stop:
                    g_callback(4);
                    break;
                }
            });

        g_initialized = true;
        return 1;
    } catch (winrt::hresult_error const& e) {
        set_last_error_hr(L"smtc_start threw", static_cast<HRESULT>(e.code()));
        g_initialized = false;
        return 0;
    } catch (...) {
        set_last_error(L"smtc_start threw an unknown exception");
        g_initialized = false;
        return 0;
    }
}

/// Register the Java callback function pointer.
__declspec(dllexport) void __cdecl smtc_set_callback(ControlCallback cb) {
    g_callback = cb;
}

/// Publish playback metadata and state.
/// artwork_data / artwork_len: raw image bytes (PNG/JPEG/...) or NULL/0 for no art.
/// The stream is handed to RandomAccessStreamReference as-is; Windows decodes it.
__declspec(dllexport) void __cdecl smtc_publish(
    const wchar_t* title,
    const wchar_t* artist,
    const wchar_t* album,
    int64_t positionMs,
    int64_t durationMs,
    bool playing,
    bool hasCurrentTrack,
    const unsigned char* artwork_data,
    int artwork_len
) {
    if (!g_smtc) return;

    try {
        auto updater = g_smtc.DisplayUpdater();
        updater.Type(MediaPlaybackType::Music);
        updater.MusicProperties().Title(winrt::hstring(title));
        updater.MusicProperties().Artist(winrt::hstring(artist));
        updater.MusicProperties().AlbumTitle(winrt::hstring(album));
        updater.Thumbnail(nullptr);

        // Set album art if available
        if (artwork_data && artwork_len > 0) {
            try {
                auto stream = InMemoryRandomAccessStream();
                auto buffer = Buffer(static_cast<uint32_t>(artwork_len));
                memcpy(buffer.data(), artwork_data, artwork_len);
                // Buffer(capacity) 只设置 Capacity，Length 仍是 0；WriteAsync 只写 Length 个字节，
                // 不显式设置 Length 会写出一个 0 字节的流（缩略图永远空白）
                buffer.Length(static_cast<uint32_t>(artwork_len));
                stream.WriteAsync(buffer).get();
                stream.Seek(0);
                auto ref = RandomAccessStreamReference::CreateFromStream(stream);
                updater.Thumbnail(ref);
            } catch (...) {
                // Artwork is best-effort
            }
        }

        updater.Update();

        // Timeline properties
        auto props = SystemMediaTransportControlsTimelineProperties();
        props.StartTime(std::chrono::milliseconds(0));
        props.Position(std::chrono::milliseconds(positionMs));
        props.EndTime(std::chrono::milliseconds(durationMs > 0 ? durationMs : 1));
        props.MinSeekTime(std::chrono::milliseconds(0));
        props.MaxSeekTime(std::chrono::milliseconds(durationMs > 0 ? durationMs : 1));
        g_smtc.UpdateTimelineProperties(props);

        // Playback status
        if (!hasCurrentTrack) {
            g_smtc.PlaybackStatus(MediaPlaybackStatus::Stopped);
        } else if (playing) {
            g_smtc.PlaybackStatus(MediaPlaybackStatus::Playing);
        } else {
            g_smtc.PlaybackStatus(MediaPlaybackStatus::Paused);
        }
    } catch (...) {
        // Best-effort
    }
}

/// Enable/disable which buttons are shown.
__declspec(dllexport) void __cdecl smtc_set_buttons(
    bool playPause, bool next, bool prev
) {
    if (!g_smtc) return;
    try {
        g_smtc.IsPlayEnabled(playPause);
        g_smtc.IsPauseEnabled(playPause);
        g_smtc.IsNextEnabled(next);
        g_smtc.IsPreviousEnabled(prev);
    } catch (...) {
    }
}

/// Release the SMTC session. Safe to call multiple times.
__declspec(dllexport) void __cdecl smtc_close() {
    if (!g_initialized) return;
    try {
        if (g_smtc) {
            g_smtc.ButtonPressed(g_buttonToken);
            g_smtc = nullptr;
        }
        g_initialized = false;
    } catch (...) {
        g_initialized = false;
    }
    g_callback = nullptr;
    // Only leave the apartment if smtc_start is the one that entered it.
    if (g_apartmentOwned) {
        g_apartmentOwned = false;
        try {
            winrt::uninit_apartment();
        } catch (...) {
        }
    }
}

/// Copy the last failure reason into buf (wide, NUL-terminated). Empty if the
/// last operation succeeded.
__declspec(dllexport) void __cdecl smtc_get_last_error(wchar_t* buf, int bufLen) {
    if (!buf || bufLen <= 0) return;
    wcsncpy_s(buf, (size_t)bufLen, g_lastError, _TRUNCATE);
}

} // extern "C"