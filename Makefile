# features, check Dockerfile
export BABASHKA_FEATURE_XML = true
export BABASHKA_FEATURE_YAML = true
export BABASHKA_FEATURE_CSV = true
export BABASHKA_FEATURE_TRANSIT =
export BABASHKA_FEATURE_JAVA_TIME = true
export BABASHKA_FEATURE_JAVA_NET_HTTP =
export BABASHKA_FEATURE_JAVA_NIO = true
export BABASHKA_FEATURE_HTTPKIT_CLIENT = true
export BABASHKA_FEATURE_HTTPKIT_SERVER =
export BABASHKA_FEATURE_CORE_MATCH = true
export BABASHKA_FEATURE_LOGGING = true
export BABASHKA_FEATURE_SQLITE =


all: clean uberjar compile

# rebase branches on top of upstream
update: clean
	@set -e
	cp -f Makefile Makefile.1
	make -f Makefile.1 update.1
	rm -f Makefile.1

update.1:
	@set -exo pipefail;
	buildbr=`git branch --show-current`
	git checkout upstream-master; git pull
	git checkout master; git merge upstream-master; git push
	git checkout msys2; git rebase master; git push --force
	for ref in `git branch --format='%(refname)'`; do
	  br=$${ref##*/}
	  [[ $${br} == feature-* ]] || continue
	  git checkout $${br}
	  git rebase msys2
	  git diff --unified=0 msys2 | tee $${br}.patch
	done
	git checkout $${buildbr}
	git rebase msys2

prepare: .feature-enabled
.feature-enabled:
	@set -exuo pipefail
	rm -f $@.swp
	for f in feature-*.patch; do
	  if grep -C 3 'Subproject commit' $$f; then echo "!!! Submodule updates in patch $$f"; exit 1; fi
	  patch -p1 -i $$f
	  feature=`echo $${f%.patch} | tr a-z- A-Z_`
	  printf "export BABASHKA_%s=true\n" $${feature} | tee -a $@.swp
	done
	mv -f $@.swp $@

uberjar: .feature-enabled
	@set -ex
	. ./.feature-enabled; script/uberjar
	java -jar target/babashka-*-standalone.jar describe

compile build: .feature-enabled
	@set -ex
	. ./.feature-enabled; script/compile

clean:
	@set -x
	for f in feature-*.patch; do
	  feature=$${f%.patch}
	  if [[ -d $${feature} ]]; then rm -rf $${feature}; fi
	done
	rm -rf .feature-enabled target
	if ! git status | grep -q 'modified:   Makefile'; then
	  git checkout .
	fi

distclean: clean
	@set -x
	git clean -x -d -f

.ONESHELL:
