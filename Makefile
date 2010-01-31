packages=$(shell ls -C java | sed 's/\s\+/:/g' | sed -e :a -e '$!N; s/\n/:/; ta')
dirs=$(shell cd java ; find . -type d -iregex '..+' \! -iregex '.*\.svn.*')
srcs=$(shell cd java ; find . -iname '*.java' \! -iregex '.*\.svn.*')
res=$(shell cd java ; find . -type f \! -iname '*.java' \! -iregex '.*\.svn.*')

dist: java-build launcher-build
	mkdir dist
	cd java-build ; jar cfe ../dist/UniLANChat.jar main.Main  *
	cp launcher-build/UniLANChat.exe dist
	cp launcher-build/UniLANChat dist

all: dist java-doc UniLANChat.zip UniLANChat.tar.gz UniLANChat-doc.tar.gz

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
	mkdir dist-zip/UniLANChat
	mkdir dist-zip/UniLANChat-doc
	cp -r dist/* dist-zip/UniLANChat
	cp -r java-doc/* dist-zip/UniLANChat-doc

UniLANChat.zip: dist-zip
	cd dist-zip ; zip -r ../UniLANChat.zip UniLANChat

UniLANChat.tar.gz: dist-zip
	cd dist-zip ; tar --owner nobody --group nobody -czf ../UniLANChat.tar.gz UniLANChat

UniLANChat-doc.tar.gz: dist-zip
	cd dist-zip ; tar --owner nobody --group nobody -czf ../UniLANChat-doc.tar.gz UniLANChat-doc

clean:
	@rm -f -r dist
	@rm -f -r java-doc
	@rm -f -r java-build
	@rm -f -r launcher-build
	@rm -f -r dist-zip
	@rm -f UniLANChat.zip
	@rm -f UniLANChat.tar.gz
	@rm -f UniLANChat-doc.tar.gz
