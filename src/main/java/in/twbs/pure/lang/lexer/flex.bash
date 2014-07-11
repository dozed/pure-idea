#!/bin/bash

cd $(dirname "$0")

if [ -z "${IDEA_HOME}" ]; then
    IDEA_HOME=~/src/idea
fi

${IDEA_HOME}/tools/lexer/jflex-1.4/bin/jflex \
    --table \
    --skel ${IDEA_HOME}/tools/lexer/idea-flex.skeleton \
    --charat --nobak \
    pure.flex
