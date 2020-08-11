#!/bin/bash

set -e

docker-compose up -d --force-recreate #--scale kafka=3
