(ns passkeys.demo.core.session.repository
  {:author "David Harrigan"}
  (:require
   [clojure.tools.logging :as log]
   [passkeys.demo.core.cache.impl :as cache]
   [passkeys.demo.system.constants :refer [passkeys-demo-session]]))

(set! *warn-on-reflection* true)

(defn ^:private lookup-session-key-wildcard
  [session-id connection]
  (some-> (str passkeys-demo-session session-id ":*")
          (cache/redis-keys connection)
          (first))) ;; there should be only ever one unique session belonging the user!!

(defn delete-session
  [session-id {{{:keys [connection]} :sessions-cache} :components :as system}]
  (when (some-> (lookup-session-key-wildcard session-id connection)
                (cache/redis-del connection))
    (log/tracef "Deleted session '%s' from the sessions cache." session-id)))

(defn get-session
  [session-id {{{:keys [connection]} :sessions-cache} :components :as system}]
  (when-let [session (some-> (lookup-session-key-wildcard session-id connection)
                             (cache/redis-get connection))]
    (log/tracef "Found session '%s' in the sessions cache." session-id)
    session))

(defn save-session
  [{:keys [session-id session] :as data} {{{:keys [connection]} :sessions-cache} :components :as system}]
  (let [session-id' (str passkeys-demo-session session-id)
        session (cache/redis-put session-id' session data connection)]
    (log/tracef "Saved session '%s' in the sessions cache." session-id')
    session))
