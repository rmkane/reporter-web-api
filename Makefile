RESET  := $(shell bash -c 'printf "\033[0m"')
BOLD   := $(shell bash -c 'printf "\033[1m"')
GREEN  := $(shell bash -c 'printf "\033[32m"')
YELLOW := $(shell bash -c 'printf "\033[33m"')
CYAN   := $(shell bash -c 'printf "\033[36m"')

SHELL := bash
.DEFAULT_GOAL := help
.PHONY: help install build package run stop debug dev test integration lint format hooks clean image up down dev-up dev-down

JAR := target/reporter-web-api-0.1.0-SNAPSHOT.jar
PID_FILE := .reporter-web-api.pid
DEBUG_OPTS := -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8787

##@ Application
help: ## Show available commands
	@printf '\n$(BOLD)Usage$(RESET)\n  make $(CYAN)<target>$(RESET)\n'
	@awk -v bold='$(BOLD)' -v reset='$(RESET)' -v green='$(GREEN)' -v yellow='$(YELLOW)' 'BEGIN { FS = ":.*##" } /^##@/ { printf "\n%s%s%s\n", bold, substr($$0, 5), reset } /^[a-zA-Z0-9_-]+:.*##/ { printf "  %s%-12s%s %s\n", green, $$1, reset, $$2 }' $(MAKEFILE_LIST)

install: ## Resolve Maven dependencies
	mvn -B -DskipTests dependency:go-offline

build: ## Compile, test, and package the jar
	mvn -B package

package: ## Package the jar without tests
	mvn -B -DskipTests package

run: package ## Start the packaged jar in the background
	@$(MAKE) --no-print-directory stop-quiet
	@mkdir -p logs
	@java -jar $(JAR) >> logs/console.out 2>&1 & echo $$! > $(PID_FILE)
	@printf '$(GREEN)Started$(RESET) %s (pid %s)\n' $(JAR) "$$(cat $(PID_FILE))"

stop: ## Stop the packaged jar
	@if [ -f $(PID_FILE) ]; then \
		pid=$$(cat $(PID_FILE)); \
		kill $$pid 2>/dev/null || true; \
		rm -f $(PID_FILE); \
		printf '$(YELLOW)Stopped$(RESET) pid %s\n' $$pid; \
	else \
		printf '$(YELLOW)Not running$(RESET)\n'; \
	fi

debug: ## Run from source with the dev profile and JDWP on port 8787
	mvn -B spring-boot:run -Dspring-boot.run.profiles=dev -Dspring-boot.run.jvmArguments="$(DEBUG_OPTS)"

dev: ## Run from source with DevTools restart and LiveReload
	mvn -B spring-boot:run -Dspring-boot.run.profiles=dev

test: ## Run the unit tests
	mvn -B test

integration: ## Run the integration tests
	mvn -B test -Pintegration

lint: ## Check Java formatting
	mvn -B spotless:check

format: ## Format Java with Google Java Format
	mvn -B spotless:apply

hooks: ## Install the git hooks
	bash scripts/setup-git-hooks.sh

clean: ## Remove build output
	mvn -B clean
	@rm -f $(PID_FILE)

##@ Docker
image: ## Build the production image
	docker compose build

up: ## Start the production compose stack
	docker compose up --build

down: ## Stop the production compose stack
	docker compose down

dev-up: ## Start the dev compose stack
	docker compose -f docker-compose.dev.yml up --build

dev-down: ## Stop the dev compose stack
	docker compose -f docker-compose.dev.yml down

stop-quiet:
	@if [ -f $(PID_FILE) ]; then kill $$(cat $(PID_FILE)) 2>/dev/null || true; rm -f $(PID_FILE); fi
