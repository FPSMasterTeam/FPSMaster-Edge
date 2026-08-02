cmake_minimum_required(VERSION 3.20)
project(fpsmaster-smtc CXX)

# Windows-only: the whole point of this target is the WinRT SMTC bridge.
if(NOT WIN32)
    message(FATAL_ERROR "fpsmaster-smtc is a Windows-only target")
endif()

# C++20, not 17: C++/WinRT's headers pull in coroutine support, and under /std:c++17
# that resolves to <experimental/coroutine>, which current MSVC rejects outright
# (STL1011). C++20 gives it the standard <coroutine> instead.
set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Architecture is selected by the generator platform, e.g.
#   cmake -B build-x64   -A x64
#   cmake -B build-win32 -A Win32
add_library(fpsmaster-smtc SHARED
    smtc.cpp
    smtc.def
)

set_target_properties(fpsmaster-smtc PROPERTIES
    PREFIX ""
    OUTPUT_NAME "fpsmaster-smtc"
)

target_compile_definitions(fpsmaster-smtc PRIVATE
    UNICODE
    _UNICODE
    WIN32_LEAN_AND_MEAN
    NOMINMAX
)

if(MSVC)
    # /EHsc: C++/WinRT throws; /MT: static CRT so the DLL does not need the
    # VC++ redistributable on the player's machine.
    target_compile_options(fpsmaster-smtc PRIVATE /EHsc /W3)
    set_property(TARGET fpsmaster-smtc PROPERTY MSVC_RUNTIME_LIBRARY "MultiThreaded")
endif()

# WindowsApp.lib provides the WinRT activation/interop entry points.
target_link_libraries(fpsmaster-smtc PRIVATE WindowsApp)
