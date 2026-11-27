cmake_minimum_required(VERSION 3.22.1)
project(klix_native C)

add_library(klix_pty SHARED
    klix_pty.c
)

target_compile_options(klix_pty PRIVATE
    -Wall
    -Wextra
    -Werror
)

target_link_options(klix_pty PRIVATE
    -Wl,-z,max-page-size=16384
)

find_library(log-lib log)
target_link_libraries(klix_pty
    ${log-lib}
)

