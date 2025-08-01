(ns passkeys.demo.ui.middleware.system
  {:author "David Harrigan"})

(set! *warn-on-reflection* true)

(defn ^:private with-system
  [handler config]
  (fn [request]
    (handler (assoc request :system config))))

(def system-middleware
  {:name ::system
   :wrap with-system})
