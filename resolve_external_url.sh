AUX=$(curl -f -s localhost:4040/api/tunnels | jq -r ".tunnels[0].public_url")

if [ "a$AUX" = "a" ]
then
  export EXTERNAL_URL=""
else
  export EXTERNAL_URL=$AUX
fi
