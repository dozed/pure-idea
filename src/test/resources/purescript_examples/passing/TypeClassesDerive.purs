module Main where

import Prelude

newtype UUID = UUID String

derive instance showUUID :: Show UUID

