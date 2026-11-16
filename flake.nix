{
  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";

  outputs =
    { self, nixpkgs }:
    let
      system = "x86_64-linux";
      pkgs = import nixpkgs { inherit system; };
      libs = with pkgs; [
        pipewire
        libglvnd
        vulkan-loader
        udev
      ];
    in
    {
      devShells.${system}.default = pkgs.mkShell {
        packages = with pkgs; [ ];

        LD_LIBRARY_PATH = pkgs.lib.makeLibraryPath libs;
      };
    };
}
