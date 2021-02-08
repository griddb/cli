/*
 	Copyright (c) 2021 TOSHIBA Digital Solutions Corporation.
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

package com.toshiba.mwcloud.gs.tools.common;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public class StatusWatcher extends AbstractWatcher {
	private final GridStoreWebAPI webapi;
	private final Set<CombinedStatus> waitForStatus;

	public StatusWatcher(GridStoreWebAPI webapi, CombinedStatus... waitForStatus) {
		this.webapi = webapi;

		Set<CombinedStatus> set = EnumSet.noneOf(CombinedStatus.class);
		set.addAll(Arrays.asList(waitForStatus));
		this.waitForStatus = Collections.unmodifiableSet(set);
	}

	public GridStoreWebAPI getWebAPI() {
		return webapi;
	}

	public Set<CombinedStatus> getWaitForStatus() {
		return waitForStatus;
	}

	@Override
	public boolean isCompleted() {
		try {
			CombinedStatus status = GridStoreCommandUtils.getCombinedStatus(webapi);
			return waitForStatus.contains(status);
		} catch (GridStoreCommandException e ){
			return false;
		}
	}

}
