(ns passkeys.demo.ui.login.mapper
  {:author "David Harrigan"})

(set! *warn-on-reflection* true)

(defn request->login
  [request]
  (let [{{{:keys [reason]} :query} :parameters} request]
    reason))
