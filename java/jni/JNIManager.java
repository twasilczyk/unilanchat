package jni;

import java.io.*;
import java.net.URL;

/**
 * Klasa odpowiada za ładowanie bibliotek natywnych z pliku jar.
 *
 * @author Tomasz Wasilczyk (www.wasilczyk.pl)
 */
public abstract class JNIManager
{
	/**
	 * Wczytanie biblioteki natywnej z pliku jar. Nazwa pliku binarnego
	 * biblioteki jest automatycznie generowana z nazwy biblioteki.
	 *
	 * @param libname nazwa biblioteki
	 * @throws NativeLibraryLoadFailed jeżeli nie udało się załadować biblioteki
	 */
	public static void loadLibrary(String libname) throws NativeLibraryLoadFailed
	{
		if (libname == null)
			throw new NullPointerException();

		String libFileName = System.mapLibraryName(libname);

		URL liburl = JNIManager.class.getResource(libFileName);
		if (liburl == null)
			throw new NativeLibraryLoadFailed(libname);

		File libTempFile;
		try
		{
			libTempFile = File.createTempFile("JNIManager-", "-" + libFileName);
			libTempFile.deleteOnExit();

			InputStream jarStream = liburl.openStream();
			FileOutputStream libTempFileStream = new FileOutputStream(libTempFile);

			byte[] buff = new byte[10240];
			int bufflen;
			while ((bufflen = jarStream.read(buff)) > 0)
				libTempFileStream.write(buff, 0, bufflen);

			jarStream.close();
			libTempFileStream.close();
		}
		catch (IOException ex)
		{
			throw new NativeLibraryLoadFailed(libname, ex);
		}

		try
		{
			System.load(libTempFile.getAbsolutePath());
		}
		catch (SecurityException ex)
		{
			throw new NativeLibraryLoadFailed(libname, ex);
		}
		catch (UnsatisfiedLinkError ex)
		{
			throw new NativeLibraryLoadFailed(libname, ex);
		}
		finally
		{
			libTempFile.delete();
		}
	}

	/**
	 * Próbuje załadować bibliotekę.
	 *
	 * @see #loadLibrary(String)
	 * @param libname nazwa biblioteki
	 * @return <code>true</code>, jeżeli udało się załadować bibliotekę
	 */
	public static boolean tryLoadLibrary(String libname)
	{
		try
		{
			JNIManager.loadLibrary(libname);
		}
		catch (NativeLibraryLoadFailed ex)
		{
			return false;
		}
		
		return true;
	}
}
