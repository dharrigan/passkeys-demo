(ns passkeys.demo.api.general.handler
  {:author "David Harrigan"}
  (:require
   [ring.util.response :as response]))

(set! *warn-on-reflection* true)

(defn not-found
  [_]
  (response/not-found "Sorry Dave, I'm afraid I can't do that."))

(defn ping
  [_]
  (response/response "Pong!"))
