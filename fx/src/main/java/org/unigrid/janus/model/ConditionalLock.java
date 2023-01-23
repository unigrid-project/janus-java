/*
    The Janus Wallet
    Copyright © 2021-2023 The Unigrid Foundation, UGD Software AB

    This program is free software: you can redistribute it and/or modify it under the terms of the
    addended GNU Affero General Public License as published by the Free Software Foundation, version 3
    of the License (see COPYING and COPYING.addendum).

    This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
    even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU Affero General Public License for more details.

    You should have received an addended copy of the GNU Affero General Public License with this program.
    If not, see <http://www.gnu.org/licenses/> and <https://github.com/unigrid-project/janus-java>.
 */

package org.unigrid.janus.model;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ConditionalLock {
	private final Lock lock = new ReentrantLock();
	private final Condition condition = lock.newCondition();

	public void wait(int timeout) {
		try {
			lock.lock();
			condition.await(timeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException ex) {
			/* Do nothing on purpose */
		} finally {
			lock.unlock();
		}
	}

	public void fire() {
		try {
			lock.lock();
			condition.signal();
		} finally {
			lock.unlock();
		}
	}
}
