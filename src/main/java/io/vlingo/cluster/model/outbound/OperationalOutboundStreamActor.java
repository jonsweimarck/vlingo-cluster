// Copyright © 2012-2018 Vaughn Vernon. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.cluster.model.outbound;

import java.util.Collection;
import java.util.Set;

import io.vlingo.actors.Actor;
import io.vlingo.cluster.model.message.Directory;
import io.vlingo.cluster.model.message.MessageConverters;
import io.vlingo.cluster.model.message.OperationalMessage;
import io.vlingo.cluster.model.message.OperationalMessageCache;
import io.vlingo.cluster.model.message.Split;
import io.vlingo.cluster.model.node.Id;
import io.vlingo.cluster.model.node.Node;
import io.vlingo.common.message.ByteBufferPool;
import io.vlingo.common.message.ByteBufferPool.PooledByteBuffer;
import io.vlingo.common.message.Converters;
import io.vlingo.common.message.RawMessage;

public class OperationalOutboundStreamActor extends Actor
  implements OperationalOutboundStream {

  private final OperationalMessageCache cache;
  private final Node node;
  private final Outbound outbound;
  
  public OperationalOutboundStreamActor(
          final Node node,
          final ManagedOutboundChannelProvider provider,
          final ByteBufferPool byteBufferPool) {
    
    this.node = node;
    this.outbound = new Outbound(provider, byteBufferPool);
    this.cache = new OperationalMessageCache(node.id(), node.name());
  }


  //===================================
  // OperationalOutbound
  //===================================

  @Override
  public void close(final Id id) {
    outbound.close(id);
  }

  @Override
  public void directory(final Set<Node> allLiveNodes) {
    final Directory dir = new Directory(node.id(), node.name(), allLiveNodes);

    final PooledByteBuffer buffer = outbound.pooledByteBuffer();
    MessageConverters.messageToBytes(dir, buffer.buffer());

    final RawMessage message = Converters.toRawMessage(node.id().value(), buffer.buffer());
    
    outbound.broadcast(outbound.bytesFrom(message, buffer));
  }

  @Override
  public void elect(final Collection<Node> allGreaterNodes) {
    outbound.broadcast(allGreaterNodes, cache.cachedRawMessage(OperationalMessage.ELECT));
  }

  @Override
  public void join() {
    outbound.broadcast(cache.cachedRawMessage(OperationalMessage.JOIN));
  }

  @Override
  public void leader() {
    outbound.broadcast(cache.cachedRawMessage(OperationalMessage.LEADER));
  }

  @Override
  public void leader(final Id id) {
    outbound.sendTo(cache.cachedRawMessage(OperationalMessage.LEADER), id);
  }

  @Override
  public void leave() {
    outbound.broadcast(cache.cachedRawMessage(OperationalMessage.LEAVE));
  }

  @Override
  public void open(final Id id) {
    outbound.open(id);
  }

  @Override
  public void ping(final Id targetNodeId) {
    outbound.sendTo(cache.cachedRawMessage(OperationalMessage.PING), targetNodeId);
  }

  @Override
  public void pulse(final Id targetNodeId) {
    outbound.sendTo(cache.cachedRawMessage(OperationalMessage.PULSE), targetNodeId);
  }

  @Override
  public void pulse() {
    outbound.broadcast(cache.cachedRawMessage(OperationalMessage.PULSE));
  }

  @Override
  public void split(final Id targetNodeId, final Id currentLeaderId) {
    final Split split = new Split(currentLeaderId);

    final PooledByteBuffer buffer = outbound.pooledByteBuffer();
    MessageConverters.messageToBytes(split, buffer.buffer());

    final RawMessage message = Converters.toRawMessage(node.id().value(), buffer.buffer());

    outbound.sendTo(outbound.bytesFrom(message, buffer), targetNodeId);
  }

  @Override
  public void vote(final Id targetNodeId) {
    outbound.sendTo(cache.cachedRawMessage(OperationalMessage.VOTE), targetNodeId);
  }


  //===================================
  // Stoppable
  //===================================
  
  public void stop() {
    outbound.close();
    
    super.stop();
  }
}
