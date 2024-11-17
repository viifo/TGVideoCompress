# 设置 CMake 最低版本
cmake_minimum_required(VERSION 3.6.0)

project("TGVideoCompress")

# JniTestLib - 设置库的名称
set(NATIVE_LIB "JniTestLib")

add_library(
        # 设置库的名称
        ${NATIVE_LIB}
        # 将库设置为共享库(共享库可执行程序运行时才载入内存，在编译过程中仅简单的引用，代码体积比较小)
        SHARED
        # 设置库源文件的相对路径
        # 相对于 jni 根目录（jniLibs.srcDir("./jni/")）
        test.cpp
)

# 搜索系统库并添加，由于 CMake 默认在搜索路径中包含系统库，因此您只需指定要添加的公共 NDK 库的名称
find_library(
        # 设置库别名
        log-lib
        # 希望添加的系统库名称
        log
)

# 关联我们自己的库和一些第三方库或者系统库
target_link_libraries(
        # 指定目标库
        ${NATIVE_LIB}
        # 将目标库链接到 NDK 中包含的日志库
        ${log-lib}
)