import { Client, type IMessage } from '@stomp/stompjs'

// Thin wrapper around a STOMP-over-WebSocket connection to the backend's /ws
// endpoint (#21). The page and the dev proxy share an origin, so the socket URL
// is derived from the current location (ws:// locally, wss:// in production).

export function stompUrl(location: Location = window.location): string {
  const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${location.host}/ws`
}

export interface TopicSubscription {
  /** Disconnects and stops reconnecting. */
  close: () => void
}

/**
 * Subscribes to one STOMP topic and keeps the subscription alive: the client
 * reconnects on its own after a dropped connection, and `onConnect` runs after
 * every (re)connect so the caller can refetch whatever it missed meanwhile.
 */
export function subscribeToTopic(
  destination: string,
  onMessage: (body: string) => void,
  onConnect?: () => void,
): TopicSubscription {
  const client = new Client({
    brokerURL: stompUrl(),
    reconnectDelay: 3000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
  })
  client.onConnect = () => {
    client.subscribe(destination, (message: IMessage) =>
      onMessage(message.body),
    )
    onConnect?.()
  }
  client.activate()
  return { close: () => void client.deactivate() }
}
