#!/usr/bin/env bash

set -euo pipefail

# Observability Command
# 
# Manages observability stack services (Prometheus, Grafana, Loki, Promtail, Postgres Exporter)

# Get script directory and project root
COMMAND_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CLI_DIR="$(cd "$COMMAND_DIR/.." && pwd)"
SCRIPTS_DIR="$(cd "$CLI_DIR/.." && pwd)"
PROJECT_ROOT="$(cd "$SCRIPTS_DIR/.." && pwd)"

# Source shared utilities
# shellcheck source=../utils.sh
source "$CLI_DIR/utils.sh"

# Docker compose file path
DOCKER_COMPOSE_FILE="$PROJECT_ROOT/infra/docker/docker-compose.local.yml"

# Observability services
O11Y_SERVICES=("prometheus" "grafana" "loki" "promtail" "postgres-exporter")

# Show help text
show_help() {
    cat << EOF
$(log "Observability Stack Management" "$BRIGHT")

Usage: $0 <subcommand> [environment]

Subcommands:
  $(log "run" "$GREEN") <env>
    Start/restart all observability services for the specified environment.
    If services are already running, they will be restarted.
    Environment: dev (default)

  $(log "stop" "$GREEN") <env>
    Stop all observability services for the specified environment.
    Environment: dev (default)

  $(log "status" "$GREEN") <env>
    Show status of observability services for the specified environment.
    Environment: dev (default)

Services managed:
  - prometheus
  - grafana
  - loki
  - promtail
  - postgres-exporter

Options:
  --help      Show this help message

Examples:
  $0 run dev
  $0 stop dev
  $0 status dev
  $0 run      # defaults to dev

For more information, see: infra/observability/README.md
EOF
}

# Check if docker-compose file exists
check_docker_compose() {
    if [[ ! -f "$DOCKER_COMPOSE_FILE" ]]; then
        log_error "Error: Docker compose file not found at $DOCKER_COMPOSE_FILE"
        exit 1
    fi
}

# Check if Docker is available
check_docker() {
    if ! command -v docker >/dev/null 2>&1; then
        log_error "Error: Docker is not installed or not in PATH"
        exit 1
    fi
    
    if ! docker info >/dev/null 2>&1; then
        log_error "Error: Docker daemon is not running"
        exit 1
    fi
}

# Execute run command
execute_run() {
    local env="${1:-dev}"
    
    check_docker
    check_docker_compose
    
    log "\n🚀 Starting observability stack for environment: $env\n" "$BRIGHT"
    
    # Build service list string
    local services_str
    services_str=$(IFS=' '; echo "${O11Y_SERVICES[*]}")
    
    # Use docker-compose up -d to start/restart services
    # This will restart services if they're already running
    if docker-compose -f "$DOCKER_COMPOSE_FILE" up -d $services_str; then
        log "\n✅ Observability stack started successfully!" "$GREEN"
        log "\nAccess points:" "$BRIGHT"
        log "  - Grafana: http://localhost:3001 (admin/admin)" "$CYAN"
        log "  - Prometheus: http://localhost:9090" "$CYAN"
        log "  - Loki: http://localhost:3100" "$CYAN"
        echo ""
    else
        log_error "\n❌ Failed to start observability stack"
        exit 1
    fi
}

# Execute stop command
execute_stop() {
    local env="${1:-dev}"
    
    check_docker
    check_docker_compose
    
    log "\n🛑 Stopping observability stack for environment: $env\n" "$BRIGHT"
    
    # Build service list string
    local services_str
    services_str=$(IFS=' '; echo "${O11Y_SERVICES[*]}")
    
    if docker-compose -f "$DOCKER_COMPOSE_FILE" stop $services_str; then
        log "\n✅ Observability stack stopped successfully!" "$GREEN"
        echo ""
    else
        log_error "\n❌ Failed to stop observability stack"
        exit 1
    fi
}

# Execute status command
execute_status() {
    local env="${1:-dev}"
    
    check_docker
    check_docker_compose
    
    log "\n📊 Observability stack status for environment: $env\n" "$BRIGHT"
    
    # Build service list string
    local services_str
    services_str=$(IFS=' '; echo "${O11Y_SERVICES[*]}")
    
    # Show status using docker-compose ps
    docker-compose -f "$DOCKER_COMPOSE_FILE" ps $services_str
    echo ""
}

# Main function
main() {
    local subcommand="${1:-}"
    
    # Handle help flags
    if [[ "$subcommand" == "--help" || "$subcommand" == "-h" || -z "$subcommand" ]]; then
        show_help
        exit 0
    fi
    
    # Get environment (default to dev)
    local env="${2:-dev}"
    
    # Shift to remove subcommand from arguments
    shift 2>/dev/null || true
    
    case "$subcommand" in
        run)
            execute_run "$env"
            ;;
        stop)
            execute_stop "$env"
            ;;
        status)
            execute_status "$env"
            ;;
        *)
            log_error "Unknown subcommand: $subcommand"
            echo ""
            show_help
            exit 1
            ;;
    esac
}

# Run main function
main "$@"

