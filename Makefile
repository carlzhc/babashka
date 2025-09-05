# -*- mode: makefile -*-
.ONESHELL:
SHELL := /bin/bash

# git remotes
origin := origin
upstream := upstream

# git branches
upstream_master_branch := upstream-master
origin_master_branch := master
msys2_branch := msys2

feature_branches += feature-clj-bom
feature_branches += feature-data-json

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

update: clean
	@set -e
	mk=`mktemp`
	trap 'rm -f $$mk' EXIT INT TERM
	cat Makefile > $$mk
	make -f $$mk update.1

update.1:
	@set -exuo pipefail
	buildbr=`git branch --show-current`
	if [[ $${buildbr} != *build* ]]; then echo "Not in build branch" >&2; exit 1; fi
	git submodule update --init # avoid submodule modification
	 # update upstream's master branch
	git fetch $(upstream) master:$(upstream_master_branch)
	 # update my master branch
	git fetch . $(upstream_master_branch):$(origin_master_branch)
	 # rebase my msys2 branch
	git rebase $(origin_master_branch) $(msys2_branch);
	 # generate patch for msys2_branch
	[[ $${MSYSTEM-} ]] && git diff --unified=0 $(origin_master_branch) | tee ${msys2_branch}.patch >/dev/null
	 # rebase feature branches
	for br in $(feature_branches); do
	  git rebase $(origin_master_branch) $${br}
	   # in $${br} branch
	  git diff --unified=0 $(origin_master_branch) | tee $${br}.patch >/dev/null
	  test -s $${br}.patch
	done
	 # rebase this build branch on top of the origin/master
	git rebase $(origin_master_branch) $${buildbr}

push:  ## Push my branches to $(origin)
	@set -exuo pipefail
	for br in `git for-each-ref refs/heads --format="%(refname:short)"`; do
	  [[ $$br == *upstream* ]] && continue
ifdef FORCE
	   git push $(origin) $$br --force
else
	   git push $(origin) $$br
endif
	done


prepare: .feature-enabled
.feature-enabled:
	@set -exuo pipefail
	rm -f $@.swp
	if [[ $${MSYSTEM-} ]]; then
	  [[ -f ${msys2_branch}.patch ]]
	  patch -p1 -i ${msys2_branch}.patch
	fi
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

help: fmt="  %-10s - %s\n"
help:
	@printf "usage: make [targets]\n"
	printf "targets:\n"
	printf $(fmt) all        "build project"
	printf $(fmt) update     "rebase msys2 and featured branches ontop of master branch"
	printf $(fmt) clean      "clean the project"
	printf $(fmt) distclean  "clean the project and run git clean to remove all other files"
