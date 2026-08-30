package com.parakore.application.service;

import com.parakore.application.dto.*;

public interface ApplicationService {

    CreateApplicationResponse create(CreateApplicationRequest request);

    ActionApplicationResponse action(ActionApplicationRequest request);

    SearchApplicationResponse search(SearchApplicationRequest request);
}

