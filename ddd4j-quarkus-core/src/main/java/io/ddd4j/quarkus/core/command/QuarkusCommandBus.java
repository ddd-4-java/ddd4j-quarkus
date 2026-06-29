package io.ddd4j.quarkus.core.command;

import io.ddd4j.core.ddd.command.DddCommandExecutor;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.fuin.cqrs4j.core.Command;
import org.fuin.cqrs4j.core.CommandExecutionFailedException;
import org.fuin.cqrs4j.core.CommandExecutor;
import org.fuin.cqrs4j.core.Result;
import org.fuin.ddd4j.core.*;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quarkus 命令总线（CDI 实现）。
 *
 * <p>自动发现所有 {@link DddCommandExecutor} 子类，按命令类型路由执行。
 *
 * <p>使用方式：
 * <pre>
 * &#64;Inject
 * QuarkusCommandBus commandBus;
 *
 * Result&lt;?&gt; result = commandBus.executeVoid(createOrderCommand);
 * </pre>
 *
 * @author <a href="https://github.com/partme-ai">PartMe.AI</a>
 * @since 2.0.x
 */
@ApplicationScoped
public class QuarkusCommandBus {

    private static final Logger log = Logger.getLogger("### DDD4J-QUARKUS : CommandBus ###");

    private final Map<Class<? extends Command>, CommandExecutor<Void, Result<?>, ?>> executorMap = new ConcurrentHashMap<>();

    @Inject
    Instance<DddCommandExecutor<?>> executors;

    void onStart(@Observes StartupEvent event) {
        for (DddCommandExecutor<?> executor : executors) {
            Set<org.fuin.ddd4j.core.EventType> commandTypes = executor.getCommandTypes();
            for (org.fuin.ddd4j.core.EventType commandType : commandTypes) {
                // 注册命令类型到执行器的映射
                // 注意：这里需要从 EventType 反推 Command 类，简化处理
                log.infof("Registered command executor: %s -> %s",
                        commandType.asString(), executor.getClass().getSimpleName());
            }
        }
        log.infof("QuarkusCommandBus initialized with %d executors", executors.stream().count());
    }

    /**
     * 执行命令（无返回值）。
     *
     * @param command 命令对象
     * @return 执行结果
     * @throws CommandExecutionFailedException 命令执行失败
     */
    public Result<?> executeVoid(Command command) throws CommandExecutionFailedException {
        return execute(command);
    }

    /**
     * 执行命令。
     *
     * @param <R>     返回值类型
     * @param command 命令对象
     * @return 执行结果
     * @throws CommandExecutionFailedException 命令执行失败
     */
    @SuppressWarnings("unchecked")
    public <R> Result<R> execute(Command command) throws CommandExecutionFailedException {
        if (command == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }

        CommandExecutor<Void, Result<?>, ?> executor = findExecutor(command);
        if (executor == null) {
            throw new CommandExecutionFailedException(
                    new IllegalStateException("No executor found for command: " + command.getClass().getName()));
        }

        try {
            @SuppressWarnings({"rawtypes", "unchecked"})
            Object raw = ((CommandExecutor) executor).execute(null, command);
            return (Result<R>) raw;
        } catch (CommandExecutionFailedException e) {
            throw e;
        } catch (Exception e) {
            // fuinorg 聚合异常（AggregateAlreadyExistsException 等）统一包装为 CommandExecutionFailedException
            throw new CommandExecutionFailedException(e);
        }
    }

    /**
     * 查找命令对应的执行器。
     */
    private CommandExecutor<Void, Result<?>, ?> findExecutor(Command command) {
        // 遍历所有注册的执行器，查找支持该命令类型的执行器
        for (DddCommandExecutor<?> executor : executors) {
            for (org.fuin.ddd4j.core.EventType commandType : executor.getCommandTypes()) {
                // 简化匹配：通过命令类名匹配
                if (commandType.asString().equals(command.getClass().getSimpleName())) {
                    return executor;
                }
            }
        }
        return null;
    }
}
