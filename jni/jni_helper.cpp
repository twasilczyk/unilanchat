#include "jni_helper.h"

#include <iostream>

using namespace std;

//TODO: używać więcej env->DeleteLocalRef(ref)

void JNI_throwException(JNIEnv *env, const string name, const string msg)
{
	string fullName = "Ljava/lang/" + name + ";";
	jclass cls = env->FindClass(fullName.c_str());
	if (cls == NULL)
	{
		string fatalMsg = "Nie mogę rzucić: " + name + "(\"" + msg + "\") - nieprawidłowa klasa wyjątku";
		env->FatalError(fatalMsg.c_str());
	}
	else if (env->ThrowNew(cls, msg.c_str()) < 0)
	{
		string fatalMsg = "Nie mogę rzucić: " + name + "(\"" + msg + "\")";
		env->FatalError(fatalMsg.c_str());
	}
	env->DeleteLocalRef(cls);
}

string JNI_jstring2string(JNIEnv *env, jstring str)
{
	const char *wartosc = env->GetStringUTFChars(str, 0);
	string wartoscStr(wartosc);
	env->ReleaseStringUTFChars(str, wartosc);

	return wartoscStr;
}

string JNI_getStringField(JNIEnv *env, jobject jthis, const string name)
{
	jclass klasa = env->GetObjectClass(jthis);
	jfieldID pole = env->GetFieldID(klasa, name.c_str(), "Ljava/lang/String;");
	jstring wartosc = (jstring)env->GetObjectField(jthis, pole);

	return JNI_jstring2string(env, wartosc);
}

void JNI_setStringField(JNIEnv *env, jobject jthis, const string name, string value)
{
	jclass klasa = env->GetObjectClass(jthis);
	jfieldID pole = env->GetFieldID(klasa, name.c_str(), "Ljava/lang/String;");
	
	env->SetObjectField(jthis, pole, env->NewStringUTF(value.c_str()));
}

int JNI_getIntField(JNIEnv *env, jobject jthis, const string name)
{
	jclass klasa = env->GetObjectClass(jthis);
	jfieldID pole = env->GetFieldID(klasa, name.c_str(), "I");
	return (jint)env->GetIntField(jthis, pole);
}

void JNI_setIntField(JNIEnv *env, jobject jthis, const string name, const int value)
{
	jclass klasa = env->GetObjectClass(jthis);
	jfieldID pole = env->GetFieldID(klasa, name.c_str(), "I");
	
	jint jvalue = value;
	
	env->SetIntField(jthis, pole, jvalue);
}

bool JNI_getBoolField(JNIEnv *env, jobject jthis, const string name)
{
	jclass klasa = env->GetObjectClass(jthis);
	jfieldID pole = env->GetFieldID(klasa, name.c_str(), "Z");
	return (jboolean)env->GetIntField(jthis, pole);
}

jobject JNI_newObject(JNIEnv *env, const string name)
{
	jclass klasa = JNI_getClass(env, name);
	jmethodID konstruktor = JNI_getMethodID(env, name, "<init>", "()V");
	
	jobject obiekt = env->NewObject(klasa, konstruktor);
	if (obiekt == NULL)
	{
		string fatalMsg = "Obiekt nie istnieje: \"" + name + "\"";
		env->FatalError(fatalMsg.c_str());
	}
	
	return obiekt;
}

jclass JNI_getClass(JNIEnv *env, const string name)
{
	jclass klasa = env->FindClass(string("L" + name + ";").c_str());
	if (klasa == NULL)
	{
		string fatalMsg = "Klasa nie istnieje: \"" + name + "\"";
		env->FatalError(fatalMsg.c_str());
	}
	
	return klasa;
}

jmethodID JNI_getMethodID(JNIEnv *env, const string className, const string methodName, const string sign)
{
	jclass klasa = JNI_getClass(env, className);
	
	jmethodID metoda = env->GetMethodID(klasa, methodName.c_str(), sign.c_str());
	if (metoda == NULL)
	{
		string fatalMsg = "Metoda nie istnieje: \"" + className + "." + methodName + ": " + sign + "\"";
		env->FatalError(fatalMsg.c_str());
	}
	
	return metoda;
}
