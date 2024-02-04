#!/bin/bash -e

. ../../include/depinfo.sh
. ../../include/path.sh

if [ "$1" == "build" ]; then
	true
elif [ "$1" == "clean" ]; then
	make clean
	exit 0
else
	exit 255
fi

# Building seperately from source tree is not supported, this means we are forced to always clean
$0 clean

# LUA_T= and LUAC_T= to disable building lua & luac
# -Dgetlocaledecpoint()=('.') fixes bionic missing decimal_point in localeconv
make CC="$CC" AR="$AR rc" RANLIB="$RANLIB" \
	MYCFLAGS="-fPIC -Dgetlocaledecpoint\(\)=\(\'.\'\)" \
	PLAT=linux LUA_T= LUAC_T= -j$cores

# TO_BIN=/dev/null disables installing lua & luac
make INSTALL=${INSTALL:-install} INSTALL_TOP="$prefix_dir" TO_BIN=/dev/null install

# make pc only generates a partial pkg-config file because ????
mkdir -p $prefix_dir/lib/pkgconfig
make pc >$prefix_dir/lib/pkgconfig/lua.pc
cat >>$prefix_dir/lib/pkgconfig/lua.pc <<'EOF'
Name: Lua
Description:
Version: ${version}
Libs: -L${libdir} -llua
Cflags: -I${includedir}
EOF
