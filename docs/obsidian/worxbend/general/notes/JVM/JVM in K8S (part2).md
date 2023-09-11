https://blog.ghaiklor.com/2018/02/20/avoid-running-nodejs-as-pid-1-under-docker-images/

It is generally recommended to use a process supervisor as the initial process (PID 1) inside a container rather than running the Java application directly as PID 1. This is because a process supervisor, such as `systemd`or `supervisord`, can perform several important tasks that are critical for the proper functioning of the container.


One of the most important tasks of the process supervisor is to handle signals properly. When a container receives a shutdown signal (such as SIGTERM), the process supervisor can ensure that the Java application is properly shut down and any resources that it is using are released before the container is stopped. If the Java application is running as PID 1, it may not handle signals correctly and can result in a misbehaving container that doesn't shut down cleanly.

Another important task of the process supervisor is to monitor the health of the Java application and restart it automatically if it crashes or becomes unresponsive. This can help ensure that the application is always available and reduces downtime.

Furthermore, a process supervisor can also provide logging and monitoring capabilities, as well as manage the lifecycle of other processes that may be running inside the container.

In summary, using a process supervisor as PID 1 in a container is recommended because it can handle signals properly, monitor the health of the Java application, provide logging and monitoring capabilities, and manage the lifecycle of other processes.


```shell

FROM openjdk:11-jre-slim

# Install process supervisor
RUN apt-get update && apt-get install -y --no-install-recommends \
    procps \
    supervisor \
    && rm -rf /var/lib/apt/lists/*

# Create a directory for the application
WORKDIR /app

# Copy the application jar file into the container
COPY myapp.jar .

# Create a configuration file for the process supervisor
RUN echo "[supervisord]" > /etc/supervisor/conf.d/supervisord.conf \
    && echo "nodaemon=true" >> /etc/supervisor/conf.d/supervisord.conf \
    && echo "" >> /etc/supervisor/conf.d/supervisord.conf \
    && echo "[program:myapp]" >> /etc/supervisor/conf.d/supervisord.conf \
    && echo "command=java -jar /app/myapp.jar" >> /etc/supervisor/conf.d/supervisord.conf \
    && echo "autostart=true" >> /etc/supervisor/conf.d/supervisord.conf \
    && echo "autorestart=true" >> /etc/supervisor/conf.d/supervisord.conf \
    && echo "user=root" >> /etc/supervisor/conf.d/supervisord.conf

# Expose the port that the application listens on
EXPOSE 8080

# Start the process supervisor when the container starts
CMD ["/usr/bin/supervisord", "-c", "/etc/supervisor/conf.d/supervisord.conf"]

```

There are several process supervisors available for running processes inside a container, and which one is considered "best" may depend on your specific use case and requirements. However, there are a few process supervisors that are commonly used and considered best practices:

 - `systemd`: systemd is a popular process supervisor that is commonly used in Linux-based systems. It provides features such as service management, process isolation, and resource management.

- `supervisord`: supervisord is a lightweight process supervisor that is commonly used in containerized environments. It provides features such as process management, logging, and monitoring.

- `runit`: runit is a simple and lightweight process supervisor that is designed to be easy to use and configure. It provides features such as service management, process management, and logging.

- `s6`: s6 is another lightweight process supervisor that is designed for use in containerized environments. It provides features such as service management, process management, and logging.

Regardless of which process supervisor you choose, it's important to ensure that it is configured properly and can handle signals properly to ensure the proper functioning of your containerized application.