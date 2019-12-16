package top.cheivin.grpc;

import lombok.extern.slf4j.Slf4j;
import top.cheivin.grpc.core.Discover;
import top.cheivin.grpc.core.GrpcRequest;
import top.cheivin.grpc.core.RemoteInstance;
import top.cheivin.grpc.exception.InstanceException;
import top.cheivin.grpc.exception.InvokeException;
import top.cheivin.grpc.handle.Caller;
import top.cheivin.grpc.handle.DefaultCaller;

/**
 * gRPC客户端
 */
@Slf4j
public class GrpcClient {
    private Discover discover;
    private Caller caller;

    public GrpcClient(Discover discover) {
        this(discover, new DefaultCaller());
    }

    public GrpcClient(Discover discover, Caller caller) {
        this.discover = discover;
        this.caller = caller;
    }

    public void start() throws Exception {
        discover.start();
        // 应用关闭时关闭监听
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    public void stop() {
        discover.close();
    }

    public final Object invoke(GrpcRequest request) throws InstanceException, InvokeException {
        return invoke(request, 1);
    }

    public final Object invoke(GrpcRequest request, int retryCount) throws InstanceException, InvokeException {
        try {
            RemoteInstance remoteInstance = discover.getInstance(request);
            return caller.call(remoteInstance, request);
        } catch (InvokeException e) {
            // 仅拦截InvokeException，用于重试
            log.error("invoke error, at time:{}", retryCount, e);
            // 最后一次条用失败则抛出异常
            if (retryCount <= 1) {
                throw e;
            }
        }
        return invoke(request, --retryCount);
    }
}