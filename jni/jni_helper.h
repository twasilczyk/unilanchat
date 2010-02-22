#include <jni.h>
#include <string>

using namespace std;

void JNI_throwException(JNIEnv *env, const string name, const string msg);

string JNI_jstring2string(JNIEnv *env, jstring str);
string JNI_getStringField(JNIEnv *env, jobject jthis, const string name);
void JNI_setStringField(JNIEnv *env, jobject jthis, const string name, const string value);
int JNI_getIntField(JNIEnv *env, jobject jthis, const string name);
void JNI_setIntField(JNIEnv *env, jobject jthis, const string name, const int value);
bool JNI_getBoolField(JNIEnv *env, jobject jthis, const string name);

jobject JNI_newObject(JNIEnv *env, const string name);
jmethodID JNI_getMethodID(JNIEnv *env, const string className, const string methodName, const string sign);
jclass JNI_getClass(JNIEnv *env, const string name);
