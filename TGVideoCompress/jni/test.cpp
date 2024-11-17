//
// Created by Jaye on 2024/11/10.
//
#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL Java_com_viifo_tgvideocompress_utils_Utilities_hello(JNIEnv *env, jobject obj) {
    std::string hello ="Hello from C++";
    return env->NewStringUTF(hello.c_str());
}