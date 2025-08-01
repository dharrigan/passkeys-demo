(ns passkeys.demo.system.config.impl
  {:author "David Harrigan"}
  (:require
   [passkeys.demo.system.config.schema :as schema]))

(set! *warn-on-reflection* true)

(defn apply-defaults
  [config]
  (schema/apply-defaults config))

(defn cookies
  [{{:keys [cookies]} :runtime-config :as system}]
  cookies)

(defn cors
  [{{:keys [cors]} :runtime-config :as app-config}]
  cors)

(defn validate
  [config]
  (schema/validate config))
