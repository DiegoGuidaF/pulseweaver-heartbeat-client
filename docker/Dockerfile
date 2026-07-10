FROM alpine:3.24

RUN apk add --no-cache curl

COPY heartbeat.sh /heartbeat.sh
RUN chmod +x /heartbeat.sh

ENTRYPOINT ["/heartbeat.sh"]
