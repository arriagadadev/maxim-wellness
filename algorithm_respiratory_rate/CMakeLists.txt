cmake_minimum_required(VERSION 3.4.1)

set(LIBS_DIR ${CMAKE_SOURCE_DIR}/libs)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
             RespiratoryRateAlgorithm

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             src/main/cpp/RespiratoryRateAlgorithm.cpp )

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
              libLog
              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

# shared lib will also be tucked into APK and sent to target
# refer to app/build.gradle, jniLibs section for that purpose.
# ${ANDROID_ABI} is handy for our purpose here. Probably this ${ANDROID_ABI} is
# the most valuable thing of this sample, the rest are pretty much normal cmake
add_library(libMxmRRM SHARED IMPORTED)
set_target_properties(libMxmRRM PROPERTIES IMPORTED_LOCATION
                      ${LIBS_DIR}/respiratory_rate/lib/${ANDROID_ABI}/libMxmRRM.so)


target_include_directories(RespiratoryRateAlgorithm PRIVATE
                           ${LIBS_DIR}/respiratory_rate/include)


target_link_libraries( # Specifies the target library.
                       RespiratoryRateAlgorithm
                       libMxmRRM
                       # Links the target library to the log library
                       # included in the NDK.
                       ${libLog} )