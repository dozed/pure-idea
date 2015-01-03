#!/bin/bash

cd $(dirname "$0")

java -jar JFlex.jar \
    --pack \
    --skel idea-flex.skeleton \
    --charat --nobak \
    pure.flex
