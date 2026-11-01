RESET  := $(shell bash -c 'printf "\033[0m"')
BOLD   := $(shell bash -c 'printf "\033[1m"')
GREEN  := $(shell bash -c 'printf "\033[32m"')
YELLOW := $(shell bash -c 'printf "\033[33m"')
CYAN   := $(shell bash -c 'printf "\033[36m"')

SHELL := bash
.DEFAULT_GOAL := help
# General
.PHONY: help hooks
# Build
.PHONY: install build package clean theme themes
# Run
.PHONY: run stop dev debug stop-quiet
# Quality
.PHONY: test integration lint format scan scan-jar scan-image
# Docker
.PHONY: image up down dev-up dev-down

JAR := target/reporter-web-api-0.1.0-SNAPSHOT.jar
IMAGE := reporter-web-api:latest
PID_FILE := .reporter-web-api.pid
DEBUG_OPTS := -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8787
THEME ?= ember

##@ General
help: ## Show available commands
	@printf '\n$(BOLD)Usage$(RESET)\n  make $(CYAN)<target>$(RESET)\n'
	@awk -v bold='$(BOLD)' -v reset='$(RESET)' -v green='$(GREEN)' -v yellow='$(YELLOW)' 'BEGIN { FS = ":.*##" } /^##@/ { printf "\n%s%s%s\n", bold, substr($$0, 5), reset } /^[a-zA-Z0-9_-]+:.*##/ { printf "  %s%-12s%s %s\n", green, $$1, reset, $$2 }' $(MAKEFILE_LIST)

hooks: ## Install the git hooks
	bash scripts/setup-git-hooks.sh

##@ Build
install: ## Resolve Maven dependencies
	mvn -B -DskipTests dependency:go-offline

build: ## Compile, test, and package the jar
	mvn -B package

package: ## Package the jar without tests
	mvn -B -DskipTests package

clean: ## Remove build output
	mvn -B clean
	@rm -f $(PID_FILE)

theme: ## Generate theme.css and the favicon from themes/$(THEME).json (THEME=ember)
	python3 scripts/theme.py $(THEME)
	python3 scripts/generate_icons.py --theme $(THEME)

themes: ## List the available UI themes
	@python3 scripts/theme.py --list

##@ Run
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

dev: ## Run from source with DevTools restart and LiveReload
	mvn -B spring-boot:run -Dspring-boot.run.profiles=dev

debug: ## Run from source with the dev profile and JDWP on port 8787
	mvn -B spring-boot:run -Dspring-boot.run.profiles=dev -Dspring-boot.run.jvmArguments="$(DEBUG_OPTS)"

##@ Quality
test: ## Run the unit tests
	mvn -B test

integration: ## Run the integration tests
	mvn -B test -Pintegration

lint: ## Check Java formatting and class annotation order
	mvn -B spotless:check
	python3 scripts/annotation_order.py --check

format: ## Format Java and fix class annotation order
	mvn -B spotless:apply
	python3 scripts/annotation_order.py --fix

scan: ## Scan the jar and the Docker image with Trivy
	@status=0; \
		$(MAKE) --no-print-directory scan-jar || status=$$?; \
		$(MAKE) --no-print-directory scan-image || status=$$?; \
		exit $$status

scan-jar: package ## Scan the jar and write reports to target/scan/jar
	@bash scripts/scan.sh jar $(JAR)

scan-image: image ## Scan the Docker image and write reports to target/scan/image
	@bash scripts/scan.sh image $(IMAGE)

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
