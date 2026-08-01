// fpsmaster-smtc - Windows System Media Transport Controls bridge DLL
// C++/WinRT native library for FPSMaster Edge music integration.
//
// Build: open in Visual Studio 2022 with "Universal Windows Platform" workload,
//        or build from command line:
//        cl /EHsc /std:c++17 /DUNICODE /D_UNICODE smtc.cpp /LD /EHsc /MD
//        /reference "C:\Program Files (x86)\Windows Kits\10\UnionMetadata\10.0.19041.0\Windows.winmd"
//        /FU "C:\Program Files (x86)\Reference Assemblies\Microsoft\Framework\.NETCore\v4.5\System.Runtime.WindowsRuntime.dll"
//        /link /OUT:fpsmaster-smtc.dll
//
// The DLL exports a tiny C ABI that the Java side (JNA) calls.
// Callbacks from SMTC button events are dispatched to a function pointer
// provided by the Java side.

#include <windows.h>
#include <stdlib.h>
#include <string.h>

// WinRT headers
#include <winrt/Windows.Foundation.h>
#include <winrt/Windows.Media.h>
#include <winrt/Windows.Storage.Streams.h>

using namespace winrt;
using namespace Windows::Media;
using namespace Windows::Storage::Streams;

// ---------------------------------------------------------------------------
// Callback types (Java sets these via JNA)
// ---------------------------------------------------------------------------
typedef void(__stdcall* ControlCallback)(int action);
static ControlCallback g_callback = nullptr;

// ---------------------------------------------------------------------------
// SMTC session state
// ---------------------------------------------------------------------------
static SystemMediaTransportControls g_smtc = nullptr;
static bool g_initialized = false;

// ---------------------------------------------------------------------------
// C ABI exports
// ---------------------------------------------------------------------------

extern "C" {

/// Initialize the SMTC session. Call once; idempotent.
/// hwnd: parent window handle (0 = use desktop window).
__declspec(dllexport) void __stdcall smtc_start(HWND hwnd) {
    if (g_initialized) return;
    try {
        // Initialize WinRT apartment
        winrt::init_apartment(winrt::apartment_type::single_threaded);

        // Get the system transport controls
        g_smtc = SystemMediaTransportControls::GetForCurrentView();
        if (!g_smtc) return;

        // Enable all supported buttons
        g_smtc.IsPlayEnabled(true);
        g_smtc.IsPauseEnabled(true);
        g_smtc.IsNextEnabled(true);
        g_smtc.IsPreviousEnabled(true);

        // Subscribe to button events
        auto token = g_smtc.ButtonPressed(
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
    } catch (...) {
        g_initialized = false;
    }
}

/// Register the Java callback function pointer.
__declspec(dllexport) void __stdcall smtc_set_callback(ControlCallback cb) {
    g_callback = cb;
}

/// Publish playback metadata and state.
/// artwork_data / artwork_len: PNG bytes or NULL/0 for no art.
__declspec(dllexport) void __stdcall smtc_publish(
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
                stream.WriteAsync(winrt::array_view<const uint8_t>(
                    artwork_data, artwork_data + artwork_len)).get();
                stream.Seek(0);
                auto ref = RandomAccessStreamReference::CreateFromStream(stream);
                updater.Thumbnail(ref);
            } catch (...) {
                // Artwork is best-effort
            }
        }

        updater.Update();

        // Timeline properties
        auto timeline = g_smtc.GetTimelineProperties();
        // Position and duration are set via the SystemMediaTransportControlsTimelineProperties
        auto props = SystemMediaTransportControlsTimelineProperties();
        props.StartTime(0);
        props.Position(std::chrono::milliseconds(positionMs));
        props.EndTime(std::chrono::milliseconds(durationMs > 0 ? durationMs : 1));
        props.MinSeekTime(0);
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
__declspec(dllexport) void __stdcall smtc_set_buttons(
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
__declspec(dllexport) void __stdcall smtc_close() {
    if (!g_initialized) return;
    try {
        if (g_smtc) {
            g_smtc.ButtonPressed({});
            g_smtc = nullptr;
        }
        g_initialized = false;
    } catch (...) {
        g_initialized = false;
    }
    g_callback = nullptr;
}

/// Get the last error message (for debugging).
__declspec(dllexport) void __stdcall smtc_get_last_error(wchar_t* buf, int bufLen) {
    (void)buf;
    (void)bufLen;
    // Simplified: just clear
    if (buf && bufLen > 0) buf[0] = 0;
}

} // extern "C"