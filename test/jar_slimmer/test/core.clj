(ns jar-slimmer.test.core
  (:use [jar-slimmer.core])
  (:use [midje.sweet]))

;; placeholder predicate for tests
(defn pred [s])

(fact (sorted-set? [])           => false
      (sorted-set? #{})          => false
      (sorted-set? (sorted-set)) => true)

(fact (first-half [1])       => (sorted-set)
      (first-half [1 2])     => (sorted-set 1)
      (first-half [1 2 3])   => (sorted-set 1)
      (first-half [1 2 3 4]) => (sorted-set 1 2))

(fact (second-half [1])       => (sorted-set 1)
      (second-half [1 2])     => (sorted-set 2)
      (second-half [1 2 3])   => (sorted-set 2 3)
      (second-half [1 2 3 4]) => (sorted-set 3 4))

(fact (compl (sorted-set :a :b :c) (sorted-set)) => (sorted-set :a :b :c))

(fact (compl (sorted-set :a :b :c) (sorted-set :b)) => (sorted-set :a :c))

(fact (true-without? #{:a :b :c} #{:b} pred) => true
      (provided (compl #{:a :b :c} #{:b}) => #{:a :c}
                (pred #{:a :c}) => true))

(defn at-least-in-2-4-or-6-8 [s]
  (let [all [2 3 4 6 7 8]
        input (apply sorted-set s)]
    (every? #(input %) all)))

(fact (at-least-in-2-4-or-6-8 [2 3 4 6 7 8])   => true
      (at-least-in-2-4-or-6-8 [2 3 4 6 7 8 9]) => true
      (at-least-in-2-4-or-6-8 [2 3 4 6 7 9])   => false)

;.;. Without work, all life goes rotten. -- Camus
(fact (find-unused (sorted-set 2 3 4 6 7 8) at-least-in-2-4-or-6-8) => (sorted-set)
      (find-unused (sorted-set 1 2 3 4 5 6 7 8 9 10) at-least-in-2-4-or-6-8) => (sorted-set 1 5 9 10))

(defn contains-at-least-2? [s] (some #(= 2 %) s))

(fact (contains-at-least-2? [1])   => nil
      (contains-at-least-2? [1 2]) => true
      (contains-at-least-2? [2])   => true)

(fact
 (find-unused (sorted-set 2) contains-at-least-2?) => (sorted-set)
 (find-unused (sorted-set 1 2) contains-at-least-2?) => (sorted-set 1)
 (find-unused (sorted-set 1 2 3) contains-at-least-2?) => (sorted-set 1 3))

;; FIXME
'(fact (smallest [] pred) => []
      (provided (pred []) => true))

;; FIXME
'(fact (smallest ["a"] (fn [x] true)) => ["a"])

;; FIXME
'(fact (smallest ["a" "b"] pred) => ["a" "b"]
      (provided (pred ["a" "b"]) => true))

;; FIXME
'(fact (smallest ["a" "b"] pred) => ["b"]
      (provided (pred ["a"]) => false
                (pred ["b"]) => true))



