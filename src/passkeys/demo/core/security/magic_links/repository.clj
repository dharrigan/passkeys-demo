(ns passkeys.demo.core.security.magic-links.repository
  {:author "David Harrigan"}
  (:require
   [clojure.tools.logging :as log]
   [passkeys.demo.core.cache.impl :as cache]
   [passkeys.demo.core.security.model.principal :as principal]
   [passkeys.demo.system.constants :refer [passkeys-demo-magic-link]]))

(set! *warn-on-reflection* true)

(defn get-magic-link
  [token {{{:keys [connection]} :magic-links-cache} :components :as system}]
  (let [magic-link-key (str passkeys-demo-magic-link token)]
    (when-let [account (cache/redis-getdel magic-link-key connection)]
      (log/debugf "Found magic link '%s' for '%s' in the magic links cache." magic-link-key (principal/email account))
      account)))

(defn save-magic-link
  [token account {{{:keys [connection ttl-seconds]} :magic-links-cache} :components :as system}]
  (let [magic-link-key (str passkeys-demo-magic-link token)]
    (cache/redis-put magic-link-key account {:expiry-seconds ttl-seconds} connection)
    (log/debugf "Saved magic link '%s' for '%s' in the magic links cache." magic-link-key (principal/email account))))
