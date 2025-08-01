(ns passkeys.demo.system.config.schema
  {:author "David Harrigan"}
  (:require
   [bling.explain :as bling]
   [malli.core :as m]
   [malli.error :as me]
   [malli.transform :as mt]
   [malli.util :as mu]))

(set! *warn-on-reflection* true)

(def ^:private Config
  [:map
   [:secrets [:map
              [:db
               [:map
                [:passkeys-demo
                 [:map
                  [:dbtype {:default "postgresql"} [:= "postgresql"]]
                  [:dbname {:default "passkeys_demo"} [:= "passkeys_demo"]]
                  [:host {:default "localhost"} :string]
                  [:port {:default 5432} [:= 5432]]
                  [:username {:default "passkey"} :string]
                  [:password {:default "itsasecret"} :string]]]]]
              [:redis
               [:map
                [:sessions [:map
                            [:uri {:default "redis://localhost/0"} :string]
                            [:ttl-seconds {:default 2592000} pos-int?]]] ;; 30 days
                [:magic-links [:map
                               [:uri {:default "redis//localhost/1"} :string]
                               [:ttl-seconds {:default 300} pos-int?]]] ;; 5 minutes
                [:passkeys [:map
                            [:uri {:default "redis//localhost/2"} :string]
                            [:ttl-seconds {:default 86400} pos-int?]]]]]]] ;; 1 day
   [:runtime-config [:map
                     [:db
                      [:map
                       [:passkeys-demo [:map
                                        [:migration-locations {:default ["db/migrations/postgresql"]} [:vector :string]]]]]]
                     [:cookies [:map
                                [:session
                                 [:map
                                  [:cookie-name {:default "passkeys-demo-session"} :string]
                                  [:ttl-seconds {:default 2592000} pos-int?]]]]] ;; 30 days
                     [:cors [:map
                             [:allow-origin {:default "https://passkeys.demo.internal"} :string]
                             [:allow-headers {:default "*"} :string]
                             [:allow-credentials {:default true} :boolean]
                             [:allow-methods {:default "CONNECT, DELETE, GET, HEAD, OPTIONS, PATCH, POST, PUT"} :string]]]
                     [:webauthn
                      [:map
                       [:passkeys
                        [:map
                         [:relying-party-id {:default "passkeys.demo.internal"} :string]
                         [:relying-party-name {:default "Passkeys Demo"} :string]]]]]
                     [:environment {:default :local} [:= :local]]
                     [:jetty {:default 3001} [:map [:port pos-int?]]]
                     [:sentry [:map [:dsn [:maybe :string]]]]
                     [:thymeleaf [:map
                                  [:html
                                   [:map
                                    [:prefix {:default "templates/html"} :string]
                                    [:suffix {:default ".html"} :string]
                                    [:cacheable {:default false} :boolean]
                                    [:cache-ttl-ms {:default 86400000} pos-int?]]]]]]]]) ;; 1 day

(defn apply-defaults
  [config]
  (m/decode Config config mt/default-value-transformer))

(defn validate
  [config]
  (-> (mu/closed-schema Config)
      (bling/explain-malli config)
      (me/humanize)))

(comment

  (require '[clojure.java.io :as io]
           '[aero.core :refer [read-config]])

  (->> (io/resource "config/config-local.edn")
       (read-config)
       (apply-defaults)
       (m/explain Config)
       (me/humanize))

  (->> (io/resource "config/config-local.edn")
       (read-config)
       (apply-defaults))

  (->> (io/resource "config/config-local.edn")
       (read-config)
       (apply-defaults)
       (validate)))
