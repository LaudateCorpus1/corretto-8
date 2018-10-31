/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */

package sun.jvm.hotspot.debugger.linux;

import java.io.*;
import java.util.*;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.debugger.cdbg.*;
import sun.jvm.hotspot.debugger.aarch64.*;
import sun.jvm.hotspot.debugger.amd64.*;
import sun.jvm.hotspot.debugger.sparc.*;
import sun.jvm.hotspot.debugger.x86.*;
import sun.jvm.hotspot.debugger.linux.aarch64.*;
import sun.jvm.hotspot.debugger.linux.amd64.*;
import sun.jvm.hotspot.debugger.linux.sparc.*;
import sun.jvm.hotspot.debugger.linux.x86.*;
import sun.jvm.hotspot.utilities.*;

class LinuxCDebugger implements CDebugger {
  private LinuxDebugger dbg;

  LinuxCDebugger(LinuxDebugger dbg) {
    this.dbg = dbg;
  }

  public List getThreadList() throws DebuggerException {
    return dbg.getThreadList();
  }

  public List/*<LoadObject>*/ getLoadObjectList() throws DebuggerException {
    return dbg.getLoadObjectList();
  }

  public LoadObject loadObjectContainingPC(Address pc) throws DebuggerException {
    if (pc == null) {
      return null;
    }
    List objs = getLoadObjectList();
    Object[] arr = objs.toArray();
    // load objects are sorted by base address, do binary search
    int mid  = -1;
    int low  = 0;
    int high = arr.length - 1;

    while (low <= high) {
       mid = (low + high) >> 1;
       LoadObject midVal = (LoadObject) arr[mid];
       long cmp = pc.minus(midVal.getBase());
       if (cmp < 0) {
          high = mid - 1;
       } else if (cmp > 0) {
          long size = midVal.getSize();
          if (cmp >= size) {
             low = mid + 1;
          } else {
             return (LoadObject) arr[mid];
          }
       } else { // match found
          return (LoadObject) arr[mid];
       }
    }
    // no match found.
    return null;
  }

  public CFrame topFrameForThread(ThreadProxy thread) throws DebuggerException {
    String cpu = dbg.getCPU();
    if (cpu.equals("x86")) {
       X86ThreadContext context = (X86ThreadContext) thread.getContext();
       Address ebp = context.getRegisterAsAddress(X86ThreadContext.EBP);
       if (ebp == null) return null;
       Address pc  = context.getRegisterAsAddress(X86ThreadContext.EIP);
       if (pc == null) return null;
       return new LinuxX86CFrame(dbg, ebp, pc);
    } else if (cpu.equals("amd64")) {
       AMD64ThreadContext context = (AMD64ThreadContext) thread.getContext();
       Address rbp = context.getRegisterAsAddress(AMD64ThreadContext.RBP);
       if (rbp == null) return null;
       Address pc  = context.getRegisterAsAddress(AMD64ThreadContext.RIP);
       if (pc == null) return null;
       return new LinuxAMD64CFrame(dbg, rbp, pc);
    } else if (cpu.equals("sparc")) {
       SPARCThreadContext context = (SPARCThreadContext) thread.getContext();
       Address sp = context.getRegisterAsAddress(SPARCThreadContext.R_SP);
       if (sp == null) return null;
       Address pc  = context.getRegisterAsAddress(SPARCThreadContext.R_O7);
       if (pc == null) return null;
       return new LinuxSPARCCFrame(dbg, sp, pc, LinuxDebuggerLocal.getAddressSize());
    } else if (cpu.equals("aarch64")) {
       AARCH64ThreadContext context = (AARCH64ThreadContext) thread.getContext();
       Address fp = context.getRegisterAsAddress(AARCH64ThreadContext.FP);
       if (fp == null) return null;
       Address pc  = context.getRegisterAsAddress(AARCH64ThreadContext.PC);
       if (pc == null) return null;
       return new LinuxAARCH64CFrame(dbg, fp, pc);
    } else {
       // Runtime exception thrown by LinuxThreadContextFactory if unknown cpu
       ThreadContext context = (ThreadContext) thread.getContext();
       return context.getTopFrame(dbg);
    }
  }

  public String getNameOfFile(String fileName) {
    return new File(fileName).getName();
  }

  public ProcessControl getProcessControl() throws DebuggerException {
    // FIXME: after stabs parser
    return null;
  }

  public boolean canDemangle() {
    return false;
  }

  public String demangle(String sym) {
    throw new UnsupportedOperationException();
  }
}
