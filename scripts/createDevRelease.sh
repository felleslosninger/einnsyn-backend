#!/bin/sh

DATETIME=`date "+%Y-%m-%dT%H_%M_%S"`
TAGNAME="DEV_${DATETIME}"

echo "Tag name: ${TAGNAME}"
git tag ${TAGNAME}
git push origin ${TAGNAME}
