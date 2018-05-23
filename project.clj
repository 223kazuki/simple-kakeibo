(defproject simple-kakeibo "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [duct/core "0.6.2"]
                 [duct/module.logging "0.3.1"]
                 [duct/module.web "0.6.4"]
                 [duct/module.ataraxy "0.2.0"]
                 [duct/module.cljs "0.3.2"]
                 [duct/module.sql "0.4.2"]
                 [org.postgresql/postgresql "42.1.4"]
                 [duct/migrator.ragtime "0.2.2"]
                 [reagent "0.8.0"]
                 [re-frame "0.10.5"]
                 [cljsjs/react-transition-group "2.3.0-0"]
                 [cljsjs/react-datepicker "1.4.1-0"]
                 [cljsjs/recharts "1.0.0-beta.10-3"]
                 [soda-ash "0.78.2" :exclusions [[cljsjs/react]]]
                 [day8.re-frame/http-fx "0.1.6"]
                 [kibu/pushy "0.3.8"]
                 [bidi "2.1.3"]
                 [buddy/buddy-auth "2.1.0"]
                 [clj-time "0.14.3"]]
  :plugins [[duct/lein-duct "0.10.6"]]
  :main ^:skip-aot simple-kakeibo.main
  :uberjar-name  "simple-kakeibo-standalone.jar"
  :resource-paths ["resources" "target/resources"]
  :prep-tasks     ["javac" "compile" ["run" ":duct/compiler"]]
  :profiles
  {:dev  [:project/dev :profiles/dev]
   :repl {:prep-tasks   ^:replace ["javac" "compile"]
          :repl-options {:init-ns user
                         :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}
   :uberjar {:aot :all}
   :profiles/dev {}
   :project/dev  {:source-paths   ["dev/src"]
                  :resource-paths ["dev/resources"]
                  :dependencies   [[integrant/repl "0.2.0"]
                                   [eftest "0.4.1"]
                                   [kerodon "0.9.0"]]}})
