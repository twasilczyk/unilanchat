packages=$(shell ls -C java | sed 's/\s\+/:/g' | sed -e :a -e '$!N; s/\n/:/; ta')
dirs=$(shell cd java ; find . -type d -iregex '..+' \! -iregex '.*\.svn.*')
srcs=$(shell cd java ; find . -iname '*.java' \! -iregex '.*\.svn.*')
res=$(shell cd java ; find . -type f \! -iname '*.java' \! -iregex '.*\.svn.*')

JNI_CPP_FLAGS = -shared -static -Wall -Wextra -O2
JNI_CPP_INC = jni/jni_helper.cpp
JNI_CPP_INCDIR = -I/usr/java/default/include -I/usr/java/default/include/linux
JAVA_BIN_DIR = /usr/java/default/bin/

dist: java-build jni-build launcher-build
	mkdir dist
	cd java-build ; jar cfme ../dist/UniLANChat.jar ../manifest.mf main.Main  *
	cp launcher-build/UniLANChat.exe dist
	cp launcher-build/UniLANChat dist

all: dist java-doc UniLANChat-bin.zip UniLANChat-bin.tar.gz UniLANChat-doc.tar.gz

netbeans: clean jni-build
	@rm -f -r java-build
	@rm -f -r jni-build
	@rm -f jni/tools_X11StartupNotification.h

java-build:
	mkdir java-build
	cd java-build ; \
	for dir in $(dirs) ; do \
		mkdir $$dir ; \
	done
	cd java ; \
	javac -Xlint:all,-serial -d ../java-build $(srcs)
	for resi in $(res) ; do \
		cp java/$$resi java-build/$$resi ; \
	done

jni-build: java-build
	mkdir jni-build
	$(JAVA_BIN_DIR)javah -d jni -classpath java-build tools.X11StartupNotification
	g++ $(JNI_CPP_FLAGS) -lX11 jni/tools_X11StartupNotification.cpp $(JNI_CPP_INC) -o jni-build/libtools_X11StartupNotification.so $(JNI_CPP_INCDIR)
	cp -f jni-build/*.so java-build/jni
	cp -f jni-build/*.so java/jni

launcher-build:
	mkdir launcher-build
	cd launcher-build ; tar xzf ../launcher/windows/launch4j-3.0.1-linux.tgz
	cd launcher-build/launch4j ; ./launch4j ../../launcher/windows/launcher.xml
	chmod -x launcher-build/UniLANChat.exe
	cp launcher/unix/UniLANChat launcher-build
	chmod +x launcher-build/UniLANChat

java-doc:
	mkdir java-doc
	javadoc -d java-doc -encoding utf8 -docencoding utf8 -charset utf8 -protected -nodeprecated -nohelp -noindex -author -windowtitle UniChat -tag todo:a:"TODO:" -sourcepath java -subpackages ${packages}

dist-zip: dist java-doc
	mkdir dist-zip
	mkdir dist-zip/UniLANChat-bin
	mkdir dist-zip/UniLANChat-doc
	cp -r dist/* dist-zip/UniLANChat-bin
	cp -r java-doc/* dist-zip/UniLANChat-doc

UniLANChat-bin.zip: dist-zip
	cd dist-zip ; zip -r ../UniLANChat-bin.zip UniLANChat-bin

UniLANChat-bin.tar.gz: dist-zip
	cd dist-zip ; tar --owner nobody --group nobody -czf ../UniLANChat-bin.tar.gz UniLANChat-bin

UniLANChat-doc.tar.gz: dist-zip
	cd dist-zip ; tar --owner nobody --group nobody -czf ../UniLANChat-doc.tar.gz UniLANChat-doc

clean:
	@rm -f -r dist
	@rm -f -r java-doc
	@rm -f -r java-build
	@rm -f -r jni-build
	@rm -f -r launcher-build
	@rm -f -r dist-zip
	@rm -f UniLANChat-bin.zip
	@rm -f UniLANChat-bin.tar.gz
	@rm -f UniLANChat-doc.tar.gz
	@rm -f jni/tools_X11StartupNotification.h
	@rm -f java/jni/*.so
	@rm -f java/jni/*.dll
