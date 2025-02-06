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

uberjar:
	script/uberjar

compile build:
	script/compile

clean:
	rm -rf target
