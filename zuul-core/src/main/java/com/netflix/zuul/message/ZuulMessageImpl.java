/**
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.zuul.message;

import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.zuul.bytebuf.ByteBufUtils;
import com.netflix.zuul.context.SessionContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;

/**
 * User: michaels@netflix.com
 * Date: 2/20/15
 * Time: 3:10 PM
 */
public class ZuulMessageImpl implements ZuulMessage, Content
{
    protected static final DynamicIntProperty MAX_BODY_SIZE_PROP = DynamicPropertyFactory.getInstance().getIntProperty(
            "zuul.message.body.max.size", 25 * 1000 * 1024);
    private static final Charset CS_UTF8 = Charset.forName("UTF-8");

    protected final SessionContext context;
    protected Headers headers;
    protected ByteBuf content = null;
    protected boolean bodyBuffered = false;
    
    public ZuulMessageImpl(SessionContext context) {
        this(context, new Headers());
    }

    public ZuulMessageImpl(SessionContext context, Headers headers) {
        this.context = context == null ? new SessionContext() : context;
        this.headers = headers == null ? new Headers() : headers;
    }

    @Override
    public SessionContext getContext() {
        return context;
    }

    @Override
    public Headers getHeaders() {
        return headers;
    }

    @Override
    public void setHeaders(Headers newHeaders) {
        this.headers = newHeaders;
    }

    @Override
    public ByteBuf content()
    {
        return this.content;
    }

    @Override
    public byte[] getBody()
    {
        return ByteBufUtils.toBytes(content);
    }

    @Override
    public void setBody(byte[] bytes)
    {
        this.content = Unpooled.wrappedBuffer(bytes);
        this.bodyBuffered = true;
    }

    @Override
    public boolean hasBody()
    {
        return content != null;
    }

    @Override
    public void setBodyAsText(String bodyText, Charset cs)
    {
        setBody(bodyText.getBytes(cs));
    }

    @Override
    public void setBodyAsText(String bodyText)
    {
        setBodyAsText(bodyText, CS_UTF8);
    }

    @Override
    public int getMaxBodySize() {
        return MAX_BODY_SIZE_PROP.get();
    }

    @Override
    public boolean isBodyBuffered() {
        return bodyBuffered;
    }

    @Override
    public void addContent(ByteBuf bb)
    {
        if (this.content == null) {
            this.content = bb;
        }
        else {
            this.content = Unpooled.wrappedBuffer(this.content, bb);
        }
    }

    @Override
    public ZuulMessage clone()
    {
        ZuulMessageImpl copy = new ZuulMessageImpl(context.clone(), headers.clone());
        return copy;
    }

    /**
     * Override this in more specific subclasses to add request/response info for logging purposes.
     *
     * @return
     */
    @Override
    public String getInfoForLogging()
    {
        return "ZuulMessage";
    }

    @RunWith(MockitoJUnitRunner.class)
    public static class UnitTest
    {
        @Test
        public void testClone()
        {
            SessionContext ctx1 = new SessionContext();
            ctx1.set("k1", "v1");
            Headers headers1 = new Headers();
            headers1.set("k1", "v1");

            ZuulMessage msg1 = new ZuulMessageImpl(ctx1, headers1);
            ZuulMessage msg2 = msg1.clone();

            assertEquals(msg1.getBody(), msg2.getBody());
            assertEquals(msg1.getHeaders(), msg2.getHeaders());
            assertEquals(msg1.getContext(), msg2.getContext());

            // Verify that values of the 2 messages are decoupled.
            msg1.getHeaders().set("k1", "v_new");
            msg1.getContext().set("k1", "v_new");

            assertEquals("v1", msg2.getHeaders().getFirst("k1"));
            assertEquals("v1", msg2.getContext().get("k1"));
        }
    }
}
