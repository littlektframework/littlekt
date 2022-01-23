#!/bin/bash
# this will convert all the files in the current dir to base64
for file in ./default_assets/*; do
  [[ $file == *.base64 ]] && continue
  base64 -w 0 "$file" > "$file".base64
done