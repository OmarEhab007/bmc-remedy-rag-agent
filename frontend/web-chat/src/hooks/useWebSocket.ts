import { useCallback, useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import type { IMessage, StompSubscription } from '@stomp/stompjs';
import type { ChatQueryMessage, ChatResponseChunk } from '../types/chat';
import { ConnectionStatus } from '../types/chat';

const WS_URL = 'http://localhost:8080/ws-chat';
const RECONNECT_DELAY_MS = 3000;
const MAX_RECONNECT_ATTEMPTS = 5;

interface UseWebSocketOptions {
  onMessage: (chunk: ChatResponseChunk) => void;
  onConnectionChange: (status: ConnectionStatus) => void;
}

interface UseWebSocketReturn {
  sendMessage: (message: ChatQueryMessage) => void;
  connectionStatus: ConnectionStatus;
  reconnect: () => void;
}

export function useWebSocket({
  onMessage,
  onConnectionChange,
}: UseWebSocketOptions): UseWebSocketReturn {
  const [connectionStatus, setConnectionStatus] = useState<ConnectionStatus>(
    ConnectionStatus.DISCONNECTED
  );
  const clientRef = useRef<Client | null>(null);
  const subscriptionRef = useRef<StompSubscription | null>(null);
  const reconnectAttemptsRef = useRef(0);
  const reconnectTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Store callbacks in refs to avoid dependency issues
  const onMessageRef = useRef(onMessage);
  const onConnectionChangeRef = useRef(onConnectionChange);

  useEffect(() => {
    onMessageRef.current = onMessage;
    onConnectionChangeRef.current = onConnectionChange;
  }, [onMessage, onConnectionChange]);

  const updateStatus = useCallback((status: ConnectionStatus) => {
    setConnectionStatus(status);
    onConnectionChangeRef.current(status);
  }, []);

  const connect = useCallback(() => {
    if (clientRef.current?.connected) {
      return;
    }

    updateStatus(ConnectionStatus.CONNECTING);

    const client = new Client({
      brokerURL: WS_URL.replace('http', 'ws') + '/websocket',
      reconnectDelay: RECONNECT_DELAY_MS,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,

      onConnect: () => {
        console.log('WebSocket connected');
        reconnectAttemptsRef.current = 0;
        updateStatus(ConnectionStatus.CONNECTED);

        // Subscribe to user-specific response queue
        subscriptionRef.current = client.subscribe(
          '/user/queue/response',
          (message: IMessage) => {
            try {
              const chunk: ChatResponseChunk = JSON.parse(message.body);
              onMessageRef.current(chunk);
            } catch (error) {
              console.error('Failed to parse message:', error);
            }
          }
        );
      },

      onStompError: (frame) => {
        console.error('STOMP error:', frame.headers['message']);
        updateStatus(ConnectionStatus.ERROR);
      },

      onWebSocketError: (event) => {
        console.error('WebSocket error:', event);
        updateStatus(ConnectionStatus.ERROR);
      },

      onDisconnect: () => {
        console.log('WebSocket disconnected');
        updateStatus(ConnectionStatus.DISCONNECTED);

        // Attempt reconnection
        if (reconnectAttemptsRef.current < MAX_RECONNECT_ATTEMPTS) {
          reconnectAttemptsRef.current++;
          console.log(
            `Reconnecting... attempt ${reconnectAttemptsRef.current}/${MAX_RECONNECT_ATTEMPTS}`
          );
          reconnectTimeoutRef.current = setTimeout(() => {
            connect();
          }, RECONNECT_DELAY_MS * reconnectAttemptsRef.current);
        }
      },
    });

    client.activate();
    clientRef.current = client;
  }, [updateStatus]);

  const disconnect = useCallback(() => {
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current);
    }

    if (subscriptionRef.current) {
      subscriptionRef.current.unsubscribe();
      subscriptionRef.current = null;
    }

    if (clientRef.current) {
      clientRef.current.deactivate();
      clientRef.current = null;
    }
  }, []);

  const sendMessage = useCallback((message: ChatQueryMessage) => {
    if (!clientRef.current?.connected) {
      console.error('Cannot send message: WebSocket not connected');
      return;
    }

    clientRef.current.publish({
      destination: '/app/chat.query',
      body: JSON.stringify(message),
    });
  }, []);

  const reconnect = useCallback(() => {
    reconnectAttemptsRef.current = 0;
    disconnect();
    setTimeout(() => connect(), 100);
  }, [connect, disconnect]);

  // Connect on mount, disconnect on unmount
  useEffect(() => {
    connect();
    return () => {
      disconnect();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return {
    sendMessage,
    connectionStatus,
    reconnect,
  };
}
