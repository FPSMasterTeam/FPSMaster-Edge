package top.fpsmaster.api.provider.client;

import top.fpsmaster.api.provider.IProvider;

public interface IConstantsProvider extends IProvider {
    String getVersion();
    String getEdition();
}