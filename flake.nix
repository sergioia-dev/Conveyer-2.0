{
  description = "A DevShells for the mail Service development";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-26.05";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs =
    {
      self,
      nixpkgs,
      flake-utils,
    }:
    flake-utils.lib.eachDefaultSystem (
      system:
      let
        pkgs = import nixpkgs {
          inherit system;
          config.allowUnfree = true;
        };
      in
      {

        devShells = {

          frontend = pkgs.mkShell {
            buildInputs = with pkgs; [
              nodejs
            ];

            shellHook = ''
              if [ -f .env ]; then
                 set -a
                 source .env
                 set +a
               fi
            '';
          };

          backend = pkgs.mkShell {
            buildInputs = with pkgs; [
              maven
              openjdk25
              nodejs
              postgresql
              redis
            ];

            JAVA_HOME = "${pkgs.openjdk25.home}";

            shellHook = ''
              if [ -f .env ]; then
                 set -a
                 source .env
                 set +a
               fi
            '';
          };
        };

        apps = {
          backend = {
            type = "app";
            program =
              let
                script = pkgs.writeShellScriptBin "run-spring" ''
                  export JAVA_HOME="${pkgs.openjdk25.home}"
                  if [ -f .env ]; then
                     set -a
                     source .env
                     set +a
                   fi
                  ./mvnw spring-boot:run
                '';
              in
              "${script}/bin/run-spring";
          };
          backend-test = {
            type = "app";
            program =
              let
                script = pkgs.writeShellScriptBin "run-spring-test" ''
                  export JAVA_HOME="${pkgs.openjdk25.home}"
                  if [ -f .env ]; then
                     set -a
                     source .env
                     set +a
                   fi
                  ./mvnw test
                '';
              in
              "${script}/bin/run-spring-test";
          };

          podman = {
            type = "app";
            program =
              let
                script = pkgs.writeShellScriptBin "run-podman" ''
                  nix develop github:sergioia-dev/nix-environments#podman
                '';
              in
              "${script}/bin/run-podman";
          };
        };
      }
    );
}
