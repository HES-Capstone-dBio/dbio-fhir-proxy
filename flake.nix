{
  description = "Dev environment for dBio-fhir-proxy";

  inputs = {
    utils.url = github:numtide/flake-utils;
    devshell.url = github:numtide/devshell;
  };

  outputs = { self, nixpkgs, utils, devshell }:
    utils.lib.eachSystem [ "x86_64-darwin" ] (system:
      let
        pkgs = import nixpkgs {
          inherit system;
          overlays = [ devshell.overlay ];
        };
      in rec {
        devShell = pkgs.devshell.mkShell {
          name = "dBio-fhir-proxy";
          packages = with pkgs; [ sbt ];
          commands = [];
        };
      }
    );
}
