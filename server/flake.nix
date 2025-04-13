{
  description = "A development flake for a Gleam project";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils, ... }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs {
          inherit system;
        };
      in {
        packages.default = pkgs.stdenv.mkDerivation {
          pname = "gleam-project";
          version = "0.1.0";
          src = ./.;
          buildInputs = [ pkgs.gleam ];
          buildPhase = ''
            echo "Building Gleam project..."
            gleam build
          '';
        };

        devShell = pkgs.mkShell {
          # env vars or shell hooks here
          buildInputs = [
            pkgs.gleam
            pkgs.erlang
            pkgs.elixir
          ];
          shellHook = ''
            alias gs='git status'
            alias gc='git commit'
            alias gp='git push'
            alias ga='git add'
          '';
        };
      }
    );
}
