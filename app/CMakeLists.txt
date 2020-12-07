# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html
#工程路径
set(pathToProject E:/yuguang/Code/UVCCode/12.7_test/USBBCTC)
#OpenCV-android-sdk路径
set(pathToOpenCV E:/yuguang/Code/UVCCode/11.26_USBBCTC/SDK)
# CMake版本信息
cmake_minimum_required(VERSION 3.4.1)
#支持-std=gnu++11
set(CMAKE_VERBOSE_MAKEFILE on)
set(CMAKE_CXX_FLAGS "-std=c++11 -O3")
#配置加载native头文件
include_directories(${pathToOpenCV}/native/jni/include)
#CPP文件夹下带编译的cpp文件
add_library(native-lib SHARED src/main/cpp/native-lib.cpp)
#动态方式加载
add_library(lib_opencv SHARED IMPORTED)

add_library(lib_ULiveness SHARED IMPORTED)

add_library(lib_android_dlib SHARED IMPORTED)

#导入so文件
set_target_properties(lib_opencv PROPERTIES IMPORTED_LOCATION ${pathToProject}/app/src/main/jniLibs/${ANDROID_ABI}/libopencv_java3.so)
set_target_properties(lib_ULiveness PROPERTIES IMPORTED_LOCATION ${pathToProject}/app/src/main/jniLibs/${ANDROID_ABI}/libULiveness.so)
set_target_properties(lib_android_dlib PROPERTIES IMPORTED_LOCATION ${pathToProject}/app/src/main/jniLibs/${ANDROID_ABI}/libandroid_dlib.so)

#C++日志
find_library(log-lib log)

find_library(
        android-lib
        android
)


target_link_libraries(
        native-lib ${log-lib}
        ${android-lib}
)


target_link_libraries(native-lib ${log-lib} lib_opencv)
target_link_libraries(native-lib ${log-lib} lib_ULiveness)
target_link_libraries(native-lib ${log-lib} lib_android_dlib)


