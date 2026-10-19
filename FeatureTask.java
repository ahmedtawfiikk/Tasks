import java.util.Set;

class FeatureTask extends Task {
    private float estimatedTime;

    public FeatureTask(String title, Set<String> tags, int id, float estimatedTime) {
        super(title, tags, id);
        this.estimatedTime = estimatedTime;
    }

    public float getEstimatedTime() {
        return estimatedTime;
    }
}
